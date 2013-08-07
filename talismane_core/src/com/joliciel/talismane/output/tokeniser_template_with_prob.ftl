[#ftl]
[#list sentence as token]
[#if token.precedingRawOutput??]
${token.precedingRawOutput}
[/#if]
${token.index?c}	${token.textForCoNLL}	${token.probability}
[/#list]

