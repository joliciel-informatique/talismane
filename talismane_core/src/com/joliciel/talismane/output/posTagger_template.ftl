[#ftl]
[#list sentence as unit]
${unit.token.index}	[#if unit.token.originalText?length==0]_[#else]${unit.token.originalText?replace(" ","_")}[/#if]	${((unit.lexicalEntry.lemma)!"_")?replace(" ","_")}	${unit.tag.code}	${(unit.lexicalEntry.category)!"_"}	${(unit.lexicalEntry.morphology)!"_"}	
[/#list]

