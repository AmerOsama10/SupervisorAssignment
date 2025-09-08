package com.supervisor.assignment.ui;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18n {
	private final ResourceBundle bundle;
	private final Locale locale;

	public I18n(Locale locale) {
		this.locale = locale;
		this.bundle = ResourceBundle.getBundle("i18n/messages", locale);
	}

	public String t(String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public String t(String key, Object... args) {
		String pattern = t(key);
		try {
			return String.format(locale, pattern, args);
		} catch (Exception e) {
			return pattern;
		}
	}
}
