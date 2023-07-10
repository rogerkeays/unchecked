
public class TestAttrErrors {
    public static void main(String [] args) {
        true.invert(); // can't dereference primitive
        "hello".greet(); // method not found
    }
}

