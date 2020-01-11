package net.tiny.ws.mvc;

import java.io.Serializable;
import java.util.Locale;

/**
 * 消息
 *
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ATTRIBUTE_NAME = Message.class.getName();

    /**
     * 类型
     */
    public static enum Type {

        /** 成功 */
        success,

        /** 警告 */
        warn,

        /** 错误 */
        error
    }

    /** 类型 */
    private Type type;

    /** 内容 */
    private String content;

    /** 参数 */
    private Object[] args;

    /** 地域 */
    private String locale;

    /**
     * 初始化一个新创建的 Message 对象，使其表示一个空消息。
     */
    public Message() {}

    /**
     * 初始化一个新创建的 Message 对象
     *
     * @param type
     *            类型
     * @param content
     *            内容
     */
    public Message(Type type, String content) {
        this.type = type;
        this.locale  = MessageResources.getInstance().getLocaleString();
        this.args = new Object[0];
        this.content = MessageResources.getInstance().getMessage(content, MessageResources.getInstance().getLocale(), args);
    }

    /**
     * @param type
     *            类型
     * @param content
     *            内容
     * @param args
     *            参数
     * @see Message#Message(Type, Locale, String, Object...)
     */
    public Message(Type type, String content, Object... args) {
        this(type, MessageResources.getInstance().getLocale(), content, args);
    }

    /**
     * @param type
     *            类型
     * @param locale
     *            地域
     * @param content
     *            内容
     * @param args
     *            参数
     */
    public Message(Type type, Locale locale, String content, Object... args) {
        this.type = type;
        setLocale(locale.getLanguage() + "_" + locale.getCountry());
        this.args = args;
        this.content = MessageResources.getInstance().getMessage(content, locale, args);
    }

    /**
     * 获取类型
     *
     * @return 类型
     */
    public Type getType() {
        return type;
    }

    /**
     * 设置类型
     *
     * @param type
     *            类型
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * 获取内容
     *
     * @return 内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置内容
     *
     * @param content
     *            内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取地域
     *
     * @return 地域
     */
    public String getLocale() {
        return this.locale;
    }

    /**
     * 设置地域
     *
     * @param locale
     *            地域
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public String toString() {
        return getContent();
    }

    /**
     * 返回成功消息
     *
     * @param content
     *            内容
     * @param args
     *            参数
     * @return 成功消息
     */
    public static Message success(String content, Object... args) {
        return new Message(Type.success, content, args);
    }

    /**
     * 返回警告消息
     *
     * @param content
     *            内容
     * @param args
     *            参数
     * @return 警告消息
     */
    public static Message warn(String content, Object... args) {
        return new Message(Type.warn, content, args);
    }

    /**
     * 返回错误消息
     *
     * @param content
     *            内容
     * @param args
     *            参数
     * @return 错误消息
     */
    public static Message error(String content, Object... args) {
        return new Message(Type.error, content, args);
    }

    /**
     * 获取国际化消息
     *
     * @param code
     *            代码
     * @param args
     *            参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Object... args) {
        return MessageResources.getInstance().getMessage(code, args);
    }
}
