
public class TestErrors {

    public static void main(String [] args) {
    
        // unreachable catch
        try {
           throw new Exception();
        } catch (Exception e) {
        } catch (Exception e) {}
    }
}
