[#ftl]
[#list sentence as unit]
[#compress]
[#if unit.token.precedingRawOutput??]${unit.token.precedingRawOutput}[/#if]
[#if unit.token.index>0]
${unit.token.index?c}	${unit.token.textForCoNLL}	${unit.posTaggedToken.lemmaForCoNLL}	${unit.tag.code}	${unit.tag.code}	${(unit.lexicalEntry.morphologyForCoNLL)!"_"}	${(unit.nonProjectiveGovernor.token.index?c)!"0"}	${unit.nonProjectiveLabel!"_"}	${(unit.governor.token.index?c)!"0"}	${unit.label!"_"}
[/#if]
[#if unit.token.trailingRawOutput??]${unit.token.trailingRawOutput}[/#if]
[/#compress]

[/#list]

