
public class TestErrors {

    public static void main(String [] args) {
        try {
           throw new Exception();
        } catch (Exception e) {
        } catch (Exception e) {} // already caught
    }
}
