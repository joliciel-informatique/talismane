[#ftl]
[#assign tIndex=tokenCount+1]
[#assign rIndex=relationCount+1]
[#assign cIndex=(sentenceCount * 6) + characterCount]
[#list sentence as unit]
[#if unit.token.index==0]
T${tIndex?c}	ROOT ${cIndex?c} ${(cIndex+4)?c}	ROOT
[#elseif unit.token.originalText?starts_with(" ")]
T${tIndex?c}	${unit.tag.code?replace("+","_")} ${(cIndex + unit.token.startIndex+6)?c} ${(cIndex +unit.token.endIndex+5)?c}	${unit.token.originalText?substring(1)}
[#elseif unit.token.originalText?ends_with(" ")]
T${tIndex?c}	${unit.tag.code?replace("+","_")} ${(cIndex + unit.token.startIndex+5)?c} ${(cIndex +unit.token.endIndex+4)?c}	${unit.token.originalText?substring(0,(unit.token.originalText?length)-1)}
[#else]
T${tIndex?c}	${unit.tag.code?replace("+","_")} ${(cIndex + unit.token.startIndex+5)?c} ${(cIndex +unit.token.endIndex+5)?c}	${unit.token.originalText}
[/#if]
[#assign tIndex=tIndex+1]
[/#list]
[#list dependencies as arc]
R${rIndex?c}	[#if arc.label??][#if arc.label?length==0]null[#else]${arc.label}[/#if][#else]null[/#if] Arg1:T${(tokenCount + arc.head.token.index+1)?c} Arg2:T${(tokenCount + arc.dependent.token.index+1)?c}
[#assign rIndex=rIndex+1]
[/#list]