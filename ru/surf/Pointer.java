package ru.surf;

public class Pointer {
    private final String filePath;
    private final long count;

    public Pointer(String filePath, long count) {
        this.count = count;
        this.filePath = filePath;
    }

    public long getCount() {
        return count;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return "Pointer {" +
                    "count=" + count + ", " +
                    "filePath='" + filePath + '\'' +
                '}';
    }
}
