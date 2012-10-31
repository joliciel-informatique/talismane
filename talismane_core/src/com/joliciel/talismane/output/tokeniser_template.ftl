[#ftl]
[#list sentence as token]
[#if token.precedingRawOutput??]
${token.precedingRawOutput}
[/#if]
${token.index}	${token.originalText?replace(" ","_")}
[/#list]

