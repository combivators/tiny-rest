package net.tiny.ws.mvc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModelAndView  implements Comparable<ModelAndView> {

    /**
     * 页面模板路径
     */
    private String viewPath;
    /**
     * 跳转页面
     */
    private String referer;
    /**
     * 验证数据
     */
    private List<Violation> violations = new ArrayList<>();

    /**
     * 即时信息
     */
    private List<Message> flashMessages = new ArrayList<>();

    /**
     * 页面输入数据
     */
    private Map<String, Object> params = new LinkedHashMap<String, Object>();

    private LinkedHashMap<String, Object> beans;

    public ModelAndView(String path) {
        setViewPath(path);
    }

    public String getViewPath() {
        return viewPath;
    }

    public void setViewPath(String viewPath) {
        this.viewPath = viewPath;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public void setViolations(List<Violation> violations) {
        this.violations = violations;
    }

    public List<Violation> removeViolations() {
        List<Violation> list = new ArrayList<>(this.violations);
        this.violations.clear();
        return list;
    }

    public boolean hasViolations() {
        return (!this.violations.isEmpty());
    }

    public List<Message> getFlashMessages() {
        return flashMessages;
    }

    public void setFlashMessages(List<Message> flashMessages) {
        this.flashMessages = flashMessages;
    }

    public void addFlashMessages(List<Message> messages) {
        this.flashMessages.addAll(messages);
    }

    public void addFlashMessage(Message message) {
        this.flashMessages.add(message);
    }

    public List<Message> removeFlashMessages() {
        List<Message> list = new ArrayList<>(this.flashMessages);
        this.flashMessages.clear();
        return list;
    }

    public boolean hasFlashMessages() {
        return (!this.flashMessages.isEmpty());
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void addParams(Map<Object, Object> params) {
        for (Map.Entry<Object, Object> entry : params.entrySet()) {
            this.params.put(entry.getKey().toString(), entry.getValue());
        }
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void setParam(String key, Object value) {
        params.put(key, value);
    }

    public Map<String, Object> removeParams() {
        Map<String, Object> map = new LinkedHashMap<String, Object>(this.params);
        this.params.clear();
        return map;
    }

    public <T> T pop(Class<T> type) {
        if (beans != null) {
            return type.cast(beans.get(getBeanName(type)));
        }
        return null;
    }

    public void push(Object bean) {
        if (beans == null) {
            beans = new LinkedHashMap<>();
        }
        beans.put(getBeanName(bean.getClass()), bean);
    }

    private <T> String getBeanName(Class<T> type) {
        String name = type.getSimpleName();
        StringBuilder sb = new StringBuilder(name.substring(0, 0).toLowerCase());
        sb.append(name.substring(1));
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if(other != null && other instanceof ModelAndView) {
            return compareTo((ModelAndView)other) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(ModelAndView other) {
        int chkView = this.viewPath.compareTo(other.getViewPath());
        if(this.referer != null && 0 == chkView) {
            return this.referer.compareTo(other.getReferer());
        }
        return chkView;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ModelAndView@").append(hashCode()).append(" ")
          .append("size=").append(params.size()).append("; ")
          .append("view=").append(viewPath).append("; ");
        if(null != referer) {
            sb.append("uri=").append(referer.toString());
        }
        if(null != violations) {
            sb.append(String.format("; violation=%1$s", violations.toString()));
        }
        if(null != params) {
            sb.append(String.format("; params=%1$s", params.toString()));
        }
        return sb.toString();
    }

}
