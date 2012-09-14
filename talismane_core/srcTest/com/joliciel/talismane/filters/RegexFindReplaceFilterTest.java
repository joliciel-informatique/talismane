///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.filters;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class RegexFindReplaceFilterTest {

	@Test
	public void testApply() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("(\\n)NbCars\\t\\d+\\n", "$1");
		String rawText = " design BLB.\nNbCars\t149253\nNbMots\t29571\nNomFichier";
		String newText = filter.apply(rawText);
		assertEquals(" design BLB.\nNbMots\t29571\nNomFichier", newText);
	}
	
	@Test
	public void testApplyBreakInMiddle() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xxx1AA";
		String text = "BBxxx2AA";
		String nextText = "BBxxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xxx1", result.get(0));
		assertEquals("CCxxx2", result.get(1));
		assertEquals("CCxxx3", result.get(2));
	}
	
	@Test
	public void testApplyReplaceAtEnd() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xxx1";
		String text = "xxx2";
		String nextText = "xAABBxxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xxx1", result.get(0));
		assertEquals("xxx2", result.get(1));
		assertEquals("xCCxxx3", result.get(2));
	}
	
	
	@Test
	public void testApplyReplaceInMiddle() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xxx1";
		String text = "xAABBxxx2";
		String nextText = "xxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xxx1", result.get(0));
		assertEquals("xCCxxx2", result.get(1));
		assertEquals("xxx3", result.get(2));
	}
	
	@Test
	public void testApplyReplaceAtStart() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xxx1AABBxxx1";
		String text = "xxx2";
		String nextText = "xxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xxx1CCxxx1", result.get(0));
		assertEquals("xxx2", result.get(1));
		assertEquals("xxx3", result.get(2));
	}
	

	@Test
	public void testApplyReplaceNotAtAll() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xxx1";
		String text = "xxx2";
		String nextText = "xxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xxx1", result.get(0));
		assertEquals("xxx2", result.get(1));
		assertEquals("xxx3", result.get(2));
	}

	@Test
	public void testApplyReplaceInMiddleAndEnd() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xxx1";
		String text = "xAABBxxx2";
		String nextText = "xAABBxxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xxx1", result.get(0));
		assertEquals("xCCxxx2", result.get(1));
		assertEquals("xCCxxx3", result.get(2));
	}
	
	@Test
	public void testApplyReplaceAcrossAllThree() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xxx1A";
		String text = "AB";
		String nextText = "Bxxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xxx1", result.get(0));
		assertEquals("CC", result.get(1));
		assertEquals("xxx3", result.get(2));
	}
	
	@Test
	public void testApplyAllOver() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xAABBxxx1AABBxxx1AA";
		String text = "BBxxx2AABBxxx2AA";
		String nextText = "BBxxx3AABBxxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xCCxxx1CCxxx1", result.get(0));
		assertEquals("CCxxx2CCxxx2", result.get(1));
		assertEquals("CCxxx3CCxxx3", result.get(2));
	}
	
	@Test
	public void testApplyReplaceAtEndOfMiddle() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xxx1";
		String text = "xxx2AABB";
		String nextText = "xxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xxx1", result.get(0));
		assertEquals("xxx2CC", result.get(1));
		assertEquals("xxx3", result.get(2));
	}
	
	@Test
	public void testApplyReplaceAtEndOfStart() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xxx1AABB";
		String text = "xxx2";
		String nextText = "xxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xxx1CC", result.get(0));
		assertEquals("xxx2", result.get(1));
		assertEquals("xxx3", result.get(2));
	}
	
	
	@Test
	public void testApplyReplaceAtAllEnds() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "xxx1AABB";
		String text = "xxx2AABB";
		String nextText = "xxx3AABB";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("xxx1CC", result.get(0));
		assertEquals("xxx2CC", result.get(1));
		assertEquals("xxx3CC", result.get(2));
	}
	

	@Test
	public void testApplyReplaceAtAllStarts() {
		RegexFindReplaceFilter filter = new RegexFindReplaceFilter("AABB", "CC");
		String prevText = "AABBxxx1";
		String text = "AABBxxx2";
		String nextText = "AABBxxx3";
		List<String> result = filter.apply(prevText, text, nextText);
		assertEquals("CCxxx1", result.get(0));
		assertEquals("CCxxx2", result.get(1));
		assertEquals("CCxxx3", result.get(2));
	}
}
