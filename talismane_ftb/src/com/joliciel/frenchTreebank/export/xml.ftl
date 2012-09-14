[#ftl]
[#macro writePhrase phrase level]
	[#list phrase.elements as element]
		[#if element.phrase]
			<${element.phraseType.code} id="${element.id?c}">
				[@writePhrase element level+1 /]
			</${element.phraseType.code}>
		[#else]
			<w cat="${element.category.code}" lemma="${element.lemma.text?xml}" mph="${element.morphology.code}" subcat="${element.subCategory.code}" id="${element.id?c}" [#if element.nextCompoundPartId!=0]next="${element.nextCompoundPartId?c}"[/#if] [#if element.previousCompoundPartId!=0]prev="${element.previousCompoundPartId?c}"[/#if] [#if element.compound && element.subunits?size>0]compound="yes"[/#if] [#if element.subunits?size>0]word="${element.word.originalText?xml}">[#else]>${element.word.originalText?xml}</w>[/#if]
			[#list element.subunits as subunit]
				<w catint="${subunit.category.code}" subcat="${subunit.subCategory.code}" mph="${subunit.morphology.code}">${subunit.word.originalText?xml}</w>
			[/#list]
			[#if element.subunits?size>0]</w>[/#if]
		[/#if]
	[/#list]
[/#macro]
<?xml version="1.0" encoding="UTF-8"?>
[#compress]
<text>
[#list file.sentences as sentence]
<SENT nb="${sentence.sentenceNumber}" id="${sentence.id?c}">
	<sentence>${sentence.text?xml}</sentence>
	${LOG.debug("Sentence number: " + sentence.sentenceNumber)!}
	[@writePhrase sentence 0 /]
</SENT>
[/#list]
</text>
[/#compress]

    