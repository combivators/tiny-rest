package net.tiny.ws.mvc;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ViolationComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ATTRIBUTE_NAME = ViolationComponent.class.getName();

    /**
     * 验证数据
     */
    private List<Violation> violations;

    /**
     * 页面输入数据
     */
    private Map<String, Object> params;


    public List<Violation> getViolations() {
        return violations;
    }

    public void setViolations(List<Violation> violations) {
        this.violations = violations;
    }

    public void clearViolations() {
        if(this.violations != null) {
            this.violations.clear();
        }
    }

    public boolean hasViolations() {
        return (this.violations != null && !this.violations.isEmpty());
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public boolean hasParams() {
        return (this.params != null && !this.params.isEmpty());
    }
}
