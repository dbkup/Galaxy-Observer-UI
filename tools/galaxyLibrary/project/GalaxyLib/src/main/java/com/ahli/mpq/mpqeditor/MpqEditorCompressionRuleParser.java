// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.mpq.mpqeditor;

import java.io.IOException;

public final class MpqEditorCompressionRuleParser {
	
	private MpqEditorCompressionRuleParser() {
		// no instances allowed
	}
	
	public static MpqEditorCompressionRule parse(final String ruleString) throws IOException {
		final char startsWith = ruleString.charAt(0);
		if (startsWith == 'M') {
			return parseRuleMask(ruleString);
		} else if (startsWith == 'S') {
			return parseRuleSize(ruleString);
		} else if (startsWith == 'D') {
			return parseRuleDefault(ruleString);
		}
		throw new IllegalArgumentException("Unknown type of rule string: " + ruleString);
	}
	
	private static MpqEditorCompressionRule parseRuleMask(final String ruleString) throws IOException {
		final int end = ruleString.indexOf('=');
		if (end < 0) {
			throw new IOException(String.format("Unable to parse Compression Rule: %s", ruleString));
		}
		final int start = ruleString.indexOf(':') + 1;
		final MpqEditorCompressionRule rule = new MpqEditorCompressionRuleMask(ruleString.substring(start, end));
		parseAbstractFields(rule, ruleString);
		return rule;
	}
	
	private static MpqEditorCompressionRule parseRuleSize(final String ruleString) throws IOException {
		final int dividerIndex = ruleString.indexOf('-');
		final int end = ruleString.indexOf('=');
		if (dividerIndex < 0 || end < 0 || dividerIndex + 1 >= end) {
			throw new IOException(String.format("Unable to parse Compression Rule: %s", ruleString));
		}
		final int start = ruleString.indexOf(':') + 1;
		final int minSize = Integer.parseInt(ruleString.substring(start, dividerIndex));
		final int maxSize = Integer.parseInt(ruleString.substring(dividerIndex + 1, end));
		final MpqEditorCompressionRule rule = new MpqEditorCompressionRuleSize(minSize, maxSize);
		parseAbstractFields(rule, ruleString);
		return rule;
	}
	
	private static MpqEditorCompressionRule parseRuleDefault(final String ruleString) {
		final MpqEditorCompressionRule rule = new MpqEditorCompressionRuleDefault();
		parseAbstractFields(rule, ruleString);
		return rule;
	}
	
	private static void parseAbstractFields(final MpqEditorCompressionRule rule, final String ruleString) {
		final String ruleStr = ruleString.trim();
		final int i = ruleStr.indexOf('=');
		if (i != -1) {
			if (ruleStr.charAt(i + 4) == '1') {
				rule.setSingleUnit(true);
			} else if (ruleStr.charAt(i + 4) == '2') {
				rule.setMarkedForDeletion(true);
			}
			
			if (ruleStr.charAt(i + 6) == '1') {
				rule.setEncrypt(true);
			} else if (ruleStr.charAt(i + 6) == '3') {
				rule.setEncrypt(true);
				rule.setEncryptAdjusted(true);
			}
			
			if (ruleStr.charAt(i + 8) == '2') {
				rule.setCompress(true);
			}
			
			rule.setCompressionMethod(parseCompressionMethod(ruleStr.substring(ruleStr.indexOf(',') + 2)));
		}
	}
	
	private static MpqEditorCompressionRuleMethod parseCompressionMethod(final String methodString) {
		if (methodString.startsWith("02", 8)) {
			return MpqEditorCompressionRuleMethod.ZLIB;
		} else if (methodString.startsWith("08", 8)) {
			return MpqEditorCompressionRuleMethod.PKWARE;
		} else if (methodString.startsWith("10", 8)) {
			return MpqEditorCompressionRuleMethod.BZIP2;
		} else if (methodString.startsWith("12", 8)) {
			return MpqEditorCompressionRuleMethod.LZMA;
		} else if (methodString.startsWith("20", 8)) {
			return MpqEditorCompressionRuleMethod.SPARSE;
		} else if (methodString.startsWith("22", 8)) {
			return MpqEditorCompressionRuleMethod.SPARSE_ZLIB;
		} else if (methodString.startsWith("30", 8)) {
			return MpqEditorCompressionRuleMethod.SPARSE_BZIP2;
		}
		return MpqEditorCompressionRuleMethod.NONE;
	}
}
