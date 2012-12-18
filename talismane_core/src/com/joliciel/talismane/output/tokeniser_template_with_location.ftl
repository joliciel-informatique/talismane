[#ftl]
[#list sentence as token]
[#if token.precedingRawOutput??]
${token.precedingRawOutput}
[/#if]
${token.index?c}	${token.originalText?replace(" ","_")}	${token.fileName}	${(token.lineNumber)?c}	${(token.columnNumber)?c}
[/#list]

