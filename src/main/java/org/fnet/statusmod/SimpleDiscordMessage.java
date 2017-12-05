package org.fnet.statusmod;

import java.util.Collections;
import java.util.List;

public class SimpleDiscordMessage {

	private String content;
	@SuppressWarnings("unused")
	private List<Object> embeds = Collections.emptyList();

	public SimpleDiscordMessage(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

}
