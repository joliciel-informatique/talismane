/*
 * Dynamic Table of Contents script
 * by Matt Whitlock <http://www.whitsoftdev.com/>
 * adapted and made more robust by Assaf Urieli
 */

function createLink(href, innerHTML) {
	var a = document.createElement("a");
	a.setAttribute("href", href);
	a.innerHTML = innerHTML;
	return a;
}

function generateTOC(toc) {
	var all = document.getElementsByTagName("*");
	var headings = [];
	for (var i=0, max=all.length; i < max; i++) {
		var node = all[i];
		var tagName = node.nodeName.toLowerCase();
		
		if (tagName == "h1" || tagName == "h2" || tagName=="h3" || tagName=="h4") {
			headings.push(node);
		}
	}
	
	var i1 = 0, i2 = 0, i3 = 0, i4 = 0;
	toc = toc.appendChild(document.createElement("ul"));
	
	for (var i=0; i<headings.length; i++) {
		var node = headings[i];
		var tagName = node.nodeName.toLowerCase();
		if (tagName == "h4") {
			++i4;
			if (i4 == 1) toc.lastChild.lastChild.lastChild.lastChild.lastChild.appendChild(document.createElement("ul"));
			var section = i1 + "." + i2 + "." + i3 + "." + i4;
			node.insertBefore(document.createTextNode(section + ". "), node.firstChild);
			node.id = "section" + section;
			toc.lastChild.lastChild.lastChild.lastChild.lastChild.lastChild.appendChild(document.createElement("li")).appendChild(createLink("#section" + section, node.innerHTML));
		} else if (tagName == "h3") {
			++i3, i4 = 0;
			if (i3 == 1) toc.lastChild.lastChild.lastChild.appendChild(document.createElement("ul"));
			var section = i1 + "." + i2 + "." + i3;
			node.insertBefore(document.createTextNode(section + ". "), node.firstChild);
			node.id = "section" + section;
			toc.lastChild.lastChild.lastChild.lastChild.appendChild(document.createElement("li")).appendChild(createLink("#section" + section, node.innerHTML));
		}
		else if (tagName == "h2") {
			++i2, i3 = 0, i4 = 0;
			if (i2 == 1) toc.lastChild.appendChild(document.createElement("ul"));
			var section = i1 + "." + i2;
			node.insertBefore(document.createTextNode(section + ". "), node.firstChild);
			node.id = "section" + section;
			toc.lastChild.lastChild.appendChild(document.createElement("li")).appendChild(createLink("#section" + section, node.innerHTML));
		}
		else if (tagName == "h1") {
			++i1, i2 = 0, i3 = 0, i4 = 0;
			var section = i1;
			node.insertBefore(document.createTextNode(section + ". "), node.firstChild);
			node.id = "section" + section;
			toc.appendChild(h1item = document.createElement("li")).appendChild(createLink("#section" + section, node.innerHTML));
		}
	}
}
