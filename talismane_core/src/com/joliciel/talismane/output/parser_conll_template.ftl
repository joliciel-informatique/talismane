[#ftl]
[#list sentence as unit]
[#if unit.token.index>0]
${unit.token.index}	${unit.token.originalText?replace(" ","_")}	${((unit.lexicalEntry.lemma)!"_")?replace(" ","_")}	${unit.tag.code}	${(unit.lexicalEntry.category)!"_"}	${(unit.lexicalEntry.morphology)!"_"}	${(unit.governor.token.index)!"_"}	${unit.label!"_"}	_	_
[/#if]
[/#list]

