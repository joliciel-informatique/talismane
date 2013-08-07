[#ftl]
[#list sentence as unit]
[#if unit.token.precedingRawOutput??]
${unit.token.precedingRawOutput}
[/#if]
[#if unit.token.index>0]
${unit.token.index?c}	${unit.token.textForCoNLL}	${unit.posTaggedToken.lemmaForCoNLL}	${unit.tag.code}	${(unit.lexicalEntry.category)!"_"}	${(unit.lexicalEntry.morphologyForCoNLL)!"_"}	${(unit.governor.token.index?c)!"0"}	${unit.label!"_"}	_	_	${(unit.posTaggedToken.token.probability * 100)?string("0.00")}	${(unit.posTaggedToken.probability * 100)?string("0.00")}	[#if (unit.arc??)]${(unit.arc.probability * 100)?string("0.00")}[#else]_[/#if]
[/#if]
[/#list]

