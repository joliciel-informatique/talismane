[#ftl]
[#list sentence as unit]
[#if unit.token.precedingRawOutput??]
${unit.token.precedingRawOutput}
[/#if]
[#if unit.token.index>0]
${unit.token.index?c}	${unit.token.textForCoNLL}	${unit.posTaggedToken.lemmaForCoNLL}	${unit.tag.code}	${unit.tag.code}	${(unit.posTaggedToken.morphologyForCoNLL)!"_"}	${(unit.nonProjectiveGovernor.token.index?c)!"0"}	${unit.nonProjectiveLabel!"_"}	${(unit.governor.token.index?c)!"0"}	${unit.label!"_"}	${(unit.posTaggedToken.token.probability * 100)?string("0.00")}	${(unit.posTaggedToken.probability * 100)?string("0.00")}	[#if (unit.arc??)]${(unit.arc.probability * 100)?string("0.00")}[#else]_[/#if]
[/#if]
[#if unit.token.trailingRawOutput??]
${unit.token.trailingRawOutput}
[/#if]
[/#list]

