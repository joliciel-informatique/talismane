[#ftl]
[#list sentence as unit]
[#compress]
[#if unit.token.precedingRawOutput??]${unit.token.precedingRawOutput}[/#if]
${(unit.token.index+1)?c}	${unit.token.textForCoNLL}	${unit.lemmaForCoNLL}	${unit.tag.code}	${unit.tag.code}	${(unit.lexicalEntry.morphologyForCoNLL)!"_"}	
[#if unit.token.trailingRawOutput??]${unit.token.trailingRawOutput}[/#if]
[/#compress]

[/#list]

