package id.my.rascal.common.template;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuccessTemplate(
    boolean isSuccess,
    String message,
    Object data,
    MetaTemplate meta
) { }
