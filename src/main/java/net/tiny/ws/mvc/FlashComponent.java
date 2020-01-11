package net.tiny.ws.mvc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FlashComponent implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String ATTRIBUTE_NAME = FlashComponent.class.getName();

	/**
	 * 即时信息
	 */
	private List<Message> messages = new ArrayList<>();

	public FlashComponent() {}

	public FlashComponent(List<Message> messages) {
		setMessages(messages);
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	public void addFlashMessage(Message message) {
		this.messages.add(message);
	}

	public void clear() {
		this.messages.clear();
	}

	public boolean hasMessages() {
		return !this.messages.isEmpty();
	}
}
