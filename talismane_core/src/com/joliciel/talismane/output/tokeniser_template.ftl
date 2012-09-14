[#ftl]
[#list sentence as token]
${token.index}	${token.originalText?replace(" ","_")}
[/#list]

