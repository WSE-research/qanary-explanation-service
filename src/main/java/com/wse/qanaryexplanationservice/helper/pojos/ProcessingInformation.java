package com.wse.qanaryexplanationservice.helper.pojos;

public class ProcessingInformation {

    private boolean docstring;
    private boolean sourcecode;

    public ProcessingInformation(boolean docstring, boolean sourcecode) {
        this.docstring = docstring;
        this.sourcecode = sourcecode;
    }

    public boolean isDocstring() {
        return docstring;
    }

    public void setDocstring(boolean docstring) {
        this.docstring = docstring;
    }

    public boolean isSourcecode() {
        return sourcecode;
    }

    public void setSourcecode(boolean sourcecode) {
        this.sourcecode = sourcecode;
    }

}
