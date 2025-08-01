import java.io.Serializable;

public class MarkerMessage implements Serializable {
    public int snapshotId;
    public String from;

    public MarkerMessage(int snapshotId, String from) {
        this.snapshotId = snapshotId;
        this.from = from;
    }

    @Override
    public String toString() {
        return "MARKER from " + from + " for snapshot " + snapshotId;
    }
}
