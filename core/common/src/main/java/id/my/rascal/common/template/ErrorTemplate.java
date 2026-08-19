package id.my.rascal.common.template;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorTemplate(
    boolean isSuccess,
    String message,
    String errorCode,
    Object errors,
    MetaTemplate meta
) { }
