import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public
    class DatabaseFeeder {

    public static void main(String[] args){
        int[] data = DatabaseFeeder.loadFromFile("circles.bin");
        DatabaseFeeder.feedDB(data);
    }
    private static void feedDB(int[] data) {
        String sql = "INSERT INTO circles (x, y,\n" +
                "r, g, b) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection("jdbc:h2:D:/Documents/LinkedInProjects/UTPTask11/dbres");
             PreparedStatement ps = conn.prepareStatement(sql);){
            for(int val : data){
                int[] res = PositionAndColor.decode(val);
                Color c = PositionAndColor.byteToColor(res[2]);
                ps.setInt(1, res[0]);
                ps.setInt(2, res[1]);
                ps.setInt(3, c.getRed());
                ps.setInt(4, c.getGreen());
                ps.setInt(5, c.getBlue());

                ps.addBatch();
            }
            ps.executeBatch();
        }catch (SQLException ex){
            ex.printStackTrace();
        }
    }

    private static int[] loadFromFile(String absolutePath) {
        int[] circles = null;
        try (
            FileChannel channel = FileChannel.open(
                Paths.get(absolutePath),
                StandardOpenOption.READ
            )
        ) {
            long fileSize = channel.size();
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);

            channel.read(buffer);
            buffer.flip();

            int circlesCount = buffer.getInt();
            circles = new int[circlesCount];

            for (int i = 0; i < circlesCount; i++)
                circles[i] = buffer.getInt();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return circles;
    }

}
