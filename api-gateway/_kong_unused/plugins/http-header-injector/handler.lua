local redis = require("resty.redis")
local cjson = require("cjson.safe")

local function stringify_list(value)
    if value == nil then
        return nil
    end

    if type(value) == "table" then
        return table.concat(value, ",")
    end

    return tostring(value)
end

local function request_id()
    return kong.request.get_header("X-Request-Id")
        or kong.request.get_header("X-Correlation-Id")
        or ngx.var.request_id
end

local function route_name()
    local route = kong.router.get_route()
    return route and route.name or nil
end

local function service_name()
    local service = kong.router.get_service()
    return service and service.name or nil
end

local HttpHeaderInjector = {
    PRIORITY = 1000,
    VERSION = "1.0.0",
}

function HttpHeaderInjector:access(conf)
    if kong.request.get_method() == "OPTIONS" then return end

    kong.service.request.clear_header(conf.session_header_user_id)
    kong.service.request.clear_header(conf.session_header_user_roles)
    kong.service.request.clear_header(conf.session_header_user_authorities)
    kong.service.request.clear_header(conf.session_header_internal_signature)

    local auth_header = kong.request.get_header("authorization")
    if not auth_header then
        return kong.response.exit(401, {
                status = 401,
                errorType = "Unauthorized",
                message = "Harap login terlebih dahulu"
            }
        )
    end

    local token = auth_header:match("^[Bb]earer%s+(.+)$")
    if not token then
        return kong.response.exit(401, {
            status = 401,
            errorType = "Unauthorized",
            message = "Format token tidak valid"
        })
    end

    local red = redis:new()
    red:set_timeouts(500, 500, 500) -- connect_timeout, send_timeout, read_timeout (ms)

    local ok, err = red:connect(conf.redis_host, conf.redis_port)
    if not ok then
        kong.log.err("[session-injector] Redis connect failed: ", err)
        return kong.response.exit(503, {
            status = 503,
            errortype = "infrastructure error",
            message = "Session store unavailable"
        })
    end

    local key = conf.redis_key_prefix .. token
    local session_raw, redis_err = red:get(key)

    if redis_err then
        local keep_alive_ok, keep_alive_err = red:set_keepalive(10000, 100)
        if not keep_alive_ok then
            kong.log.warn("[session-injector] Redis set_keepalive failed: ", keep_alive_err)
        end
        kong.log.err("[session-injector] Redis GET error: ", redis_err)
        return kong.response.exit(503, {
            status = 503,
            errortype = "infrastructure error",
            message = "Session store error"
        })
    end

    if not session_raw or session_raw == ngx.null then
        local keep_alive_ok, keep_alive_err = red:set_keepalive(10000, 100)
        if not keep_alive_ok then
            kong.log.warn("[session-injector] Redis set_keepalive failed: ", keep_alive_err)
        end
        return kong.response.exit(401, {
            status = 401,
            errorType = "Unauthorized",
            message = "Session not found or expired"
        })
    end

    local session, decode_err = cjson.decode(session_raw)
    if not session then
        local keep_alive_ok, keep_alive_err = red:set_keepalive(10000, 100)
        if not keep_alive_ok then
            kong.log.warn("[session-injector] Redis set_keepalive failed: ", keep_alive_err)
        end
        kong.log.err("[session-injector] JSON decode failed: ", decode_err)
        return kong.response.exit(500, {
            status = 500,
            errorType = "Internal Server Error",
            message = "Malformed session data"
        })
    end

    local subject = session.subject or session.userId or session.user_id
    if not subject then
        local keep_alive_ok, keep_alive_err = red:set_keepalive(10000, 100)
        if not keep_alive_ok then
            kong.log.warn("[session-injector] Redis set_keepalive failed: ", keep_alive_err)
        end
        kong.log.err("[session-injector] Session subject missing")
        return kong.response.exit(500, {
            status = 500,
            errorType = "Internal Server Error",
            message = "Malformed session data"
        })
    end

    local banned, ban_err = red:sismember(conf.redis_ban_key, tostring(subject))
    local keep_alive_ok, keep_alive_err = red:set_keepalive(10000, 100)
    if not keep_alive_ok then
        kong.log.warn("[session-injector] Redis set_keepalive failed: ", keep_alive_err)
    end

    if ban_err then
        kong.log.err("[session-injector] Redis ban check error: ", ban_err)
        return kong.response.exit(503, {
            status = 503,
            errortype = "infrastructure error",
            message = "Session store error"
        })
    end

    if banned == 1 then
        return kong.response.exit(403, {
            status = 403,
            errorType = "Forbidden",
            message = "User is banned"
        })
    end

    kong.service.request.set_header(
        conf.session_header_user_id,
        tostring(subject)
    )

    -- Logging
    local roles = stringify_list(session.roles)
    if roles then
        kong.service.request.set_header(conf.session_header_user_roles, roles)
    end

    local authorities = stringify_list(session.authorities)
    if authorities then
        kong.service.request.set_header(conf.session_header_user_authorities, authorities)
    end

    kong.service.request.set_header(
        conf.session_header_internal_signature,
        "atlanta331"
    )

    kong.service.request.clear_header("authorization")
    kong.service.request.clear_header("Authorization")

    kong.ctx.shared.request_audit = {
        user_id = tostring(subject),
        roles = roles,
        session_id_hint = string.sub(token, 1, 8)
    }

    kong.log.debug("[session-injector] Injected session for user: ", subject)

end

function HttpHeaderInjector:log(conf)
    if not conf.audit_enabled then return end

    local audit = kong.ctx.shared.request_audit or {}
    local serialized = kong.log.serialize()
    local entry = {
        type = "request_audit",
        timestamp = ngx.now(),
        request_id = request_id(),
        user_id = audit.user_id,
        roles = audit.roles,
        session_id_hint = audit.session_id_hint,
        client_ip = kong.client.get_forwarded_ip(),
        method = kong.request.get_method(),
        path = kong.request.get_path(),
        query = kong.request.get_raw_query(),
        status = kong.response.get_status(),
        route = route_name(),
        service = service_name(),
        latencies = serialized and serialized.latencies or nil,
        user_agent = kong.request.get_header("User-Agent")
    }

    local encoded = cjson.encode(entry)
    if conf.audit_log_level == "warn" then
        kong.log.warn(encoded)
    elseif conf.audit_log_level == "info" then
        kong.log.info(encoded)
    else
        kong.log.notice(encoded)
    end
end

return HttpHeaderInjector

