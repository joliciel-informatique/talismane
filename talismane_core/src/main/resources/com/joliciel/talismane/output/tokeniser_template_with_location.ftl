[#ftl]
[#list sentence as token]
[#if token.precedingRawOutput??]
${token.precedingRawOutput}
[/#if]
${(token.index+1)?c}	${token.textForCoNLL}	${token.fileName}	${(token.lineNumber+1)?c}	${(token.columnNumber+1)?c}	${(unit.token.lineNumberEnd+1)?c}	${(unit.token.columnNumberEnd+1)?c}
[#if token.trailingRawOutput??]
${token.trailingRawOutput}
[/#if]
[/#list]

