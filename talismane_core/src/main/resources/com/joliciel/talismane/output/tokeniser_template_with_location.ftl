[#ftl]
[#list sentence as token]
[#if token.precedingRawOutput??]
${token.precedingRawOutput}
[/#if]
${(token.index+1)?c}	${token.textForCoNLL}	${token.fileName}	${(token.lineNumber)?c}	${(token.columnNumber)?c}	${(unit.token.lineNumberEnd)?c}	${(unit.token.columnNumberEnd)?c}
[#if token.trailingRawOutput??]
${token.trailingRawOutput}
[/#if]
[/#list]

