package surf.code.review.task;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Pointer {
    private final String filePath;
    private long count;

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

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(filePath)
                .append(count)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Pointer pointer = (Pointer) o;

        return new EqualsBuilder().append(count, pointer.count).append(filePath, pointer.filePath).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(filePath).append(count).toHashCode();
    }
}
