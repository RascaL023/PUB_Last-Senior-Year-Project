return {
    name = "http-header-injector",
    fields = {
        { config = {
            type = "record",
            fields = {
                { redis_host = {
                    type = "string",
                    default = "127.0.0.1"
                }},
                { redis_port = {
                    type = "number",
                    default = 6379
                }},
                { redis_key_prefix = {
                    type = "string",
                    default = "session:"
                }},
                { redis_ban_key = {
                    type = "string",
                    default = "banned:users"
                }},

                { session_header_user_id = {
                    type = "string",
                    default = "X-User-Id"
                }},
                { session_header_user_roles = {
                    type = "string",
                    default = "X-User-Roles"
                }},
                { session_header_user_authorities = {
                    type = "string",
                    default = "X-User-Authorities"
                }},
                { session_header_internal_signature = {
                    type = "string",
                    default = "X-Internal-Signature"
                }},
                { audit_enabled = {
                    type = "boolean",
                    default = true
                }},
                { audit_log_level = {
                    type = "string",
                    default = "notice",
                    one_of = { "notice", "info", "warn" }
                }},
            }
        }}
    }
}

