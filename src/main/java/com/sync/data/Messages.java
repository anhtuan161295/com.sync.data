package com.sync.data;

import org.opencms.i18n.A_CmsMessageBundle;
import org.opencms.i18n.I_CmsMessageBundle;

public class Messages extends A_CmsMessageBundle {

	public static final String GUI_NEWSLETTER_MASSMAIL_TOOL_NAME_0 = "GUI_NEWSLETTER_MASSMAIL_TOOL_NAME_0";
	public static final String ERR_PROBLEM_SENDING_EMAIL_0 = "ERR_PROBLEM_SENDING_EMAIL_0";
	public static final String ERR_BAD_SENDER_ADDRESS_0 = "ERR_BAD_SENDER_ADDRESS_0";
	public static final String ERR_BAD_SUBJECT_FIELD_0 = "ERR_BAD_SUBJECT_FIELD_0";
	public static final String ERR_BAD_SUBJECT_MESSAGE_0 = "ERR_BAD_SUBJECT_MESSAGE_0";
	public static final String ERR_BAD_TO_GROUP_0 = "ERR_BAD_TO_GROUP_0";
	public static final String GUI_USER_EDITOR_LABEL_IDENTIFICATION_BLOCK_COMPOSE_EMAIL_0 = "GUI_USER_EDITOR_LABEL_IDENTIFICATION_BLOCK_COMPOSE_EMAIL_0";
	public static final String MODULE_DATE_FORMAT = "MODULE_DATE_FORMAT";
	public static final String MODULE_EMAIL_SUBJECT_PREFIX = "MODULE_EMAIL_SUBJECT_PREFIX";

	private static final String BUNDLE_NAME = "com.clicksandlinks.opencms.groupmail.workplace";

	private static final I_CmsMessageBundle INSTANCE = new Messages();

	private Messages() {

		// hide the constructor
	}

	public static I_CmsMessageBundle get() {

		return INSTANCE;
	}

	public String getBundleName() {

		return BUNDLE_NAME;
	}
}
