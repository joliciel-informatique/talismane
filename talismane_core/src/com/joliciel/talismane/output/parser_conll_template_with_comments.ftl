[#ftl]
[#list sentence as unit]
[#if unit.token.precedingRawOutput??]
${unit.token.precedingRawOutput}
[/#if]
[#if unit.token.index>0]
${unit.token.index?c}	${unit.token.textForCoNLL}	${unit.posTaggedToken.lemmaForCoNLL}	${unit.tag.code}	${(unit.lexicalEntry.category)!"_"}	${(unit.lexicalEntry.morphologyForCoNLL)!"_"}	${(unit.governor.token.index?c)!"0"}	${unit.label!"_"}	_	_	${unit.posTaggedToken.comment}	[#if (unit.arc??)]${unit.arc.comment}[/#if]
[/#if]
[/#list]

