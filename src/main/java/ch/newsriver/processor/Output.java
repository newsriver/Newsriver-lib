package ch.newsriver.processor;

/**
 * Created by eliapalme on 04/05/16.
 */
public  class Output<I,O> {

    private O output;
    private I intput;
    private boolean success=false;
    private boolean update=false;

    public O getOutput() {
        return output;
    }

    public void setOutput(O output) {
        this.output = output;
    }

    public I getIntput() {
        return intput;
    }

    public void setIntput(I intput) {
        this.intput = intput;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }
}
