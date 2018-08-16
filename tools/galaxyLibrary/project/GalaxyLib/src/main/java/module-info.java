module GalaxyLib {
	requires transitive java.xml;
	requires transitive com.fasterxml.jackson.annotation;
	requires transitive org.apache.commons.configuration2;
	requires transitive org.apache.commons.io;
	requires transitive org.apache.commons.lang3;
	requires transitive org.apache.logging.log4j;
	requires transitive vtd.xml;
	
	exports com.ahli.galaxy;
	exports com.ahli.galaxy.archive;
	exports com.ahli.galaxy.game;
	exports com.ahli.galaxy.game.def;
	exports com.ahli.galaxy.game.def.abstracts;
	exports com.ahli.galaxy.parser;
	exports com.ahli.galaxy.parser.abstracts;
	exports com.ahli.galaxy.parser.interfaces;
	exports com.ahli.galaxy.ui;
	exports com.ahli.galaxy.ui.abstracts;
	exports com.ahli.galaxy.ui.exception;
	exports com.ahli.galaxy.ui.interfaces;
	exports com.ahli.mpq;
	exports com.ahli.mpq.i18n;
	exports com.ahli.mpq.mpqeditor;
	exports com.ahli.util;
}