[#ftl]
[#list sentence as unit]
[#if unit.token.precedingRawOutput??]
${unit.token.precedingRawOutput}
[/#if]
${(unit.token.index+1)?c}	${unit.token.textForCoNLL}	${unit.lemmaForCoNLL}	${unit.tag.code}	${(unit.lexicalEntry.category)!"_"}	${(unit.lexicalEntry.morphologyForCoNLL)!"_"}	${(unit.token.probability * 100)?string("0.00")}	${(unit.probability * 100)?string("0.00")}	
[/#list]

