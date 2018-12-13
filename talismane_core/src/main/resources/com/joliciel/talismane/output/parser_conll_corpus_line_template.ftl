[#ftl]
[#list sentence as unit]
[#if unit.precedingRawOutput??]
${unit.precedingRawOutput}
[/#if]
[#if unit.index>0]
${unit.index?c}	${unit.token}	${unit.lemma}	${unit.posTag}	${unit.posTag}	${unit.morphology!"_"}	${unit.nonProjGovernorIndex?c!"0"}	${unit.nonProjLabel!"_"}	${unit.governorIndex?c!"0"}	${unit.label!"_"}
[/#if]
[#if unit.trailingRawOutput??]
${unit.trailingRawOutput}
[/#if]
[/#list]

