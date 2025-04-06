package org.xminicraft.xminicraftlauncher.util;

import java.util.Objects;

public class Task {
    private String taskStatus = "";
    private int current = 0;
    private int total = 0;
    protected volatile boolean aborted;
    public final Signal<String> status = new Signal<>();
    public final Signal<TaskProgress> progress = new Signal<>();
    public final Signal<Void> finished = new Signal<>();
    public final Signal<Void> abortFinished = new Signal<>();

    public String getTitle() {
        return "Task";
    }

    public void run() {
        for (int i = 0; i < 100; ++i) {
            if (this.aborted) {
                this.abortFinished.emit();
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            this.setStatus("File " + (i + 1));
            this.setProgress(i + 1, 100);
        }
    }

    public String getStatus() {
        return this.taskStatus;
    }

    public int getCurrentProgress() {
        return this.current;
    }

    public int getTotalProgress() {
        return this.total;
    }

    public void setStatus(String text) {
        if (!Objects.equals(this.taskStatus, text)) {
            this.taskStatus = text;
            this.status.emit(text);
        }
    }

    public void setProgress(int current, int total) {
        if (this.current != current || this.total != total) {
            this.current = current;
            this.total = total;
            this.progress.emit(new TaskProgress(current, total));
        }
    }

    public void abort() {
        this.aborted = true;
    }

    public static class TaskProgress {
        public final int current;
        public final int total;

        public TaskProgress(int current, int total) {
            this.current = current;
            this.total = total;
        }
    }
}
