[#ftl]
[#list sentence as token]
[#if token.precedingRawOutput??]
${token.precedingRawOutput}
[/#if]
${(token.index+1)?c}	${token.textForCoNLL}	${token.probability}
[#if token.trailingRawOutput??]
${token.trailingRawOutput}
[/#if]
[/#list]

