public class JavaCheck {
    private static final String[] CHECKED_PROPERTIES = {"os.arch", "java.version", "java.vendor", "java.runtime.version"};

    public static void main(String[] args) {
        for (String property : CHECKED_PROPERTIES) {
            String value = System.getProperty(property);
            if (value != null) {
                System.out.println(property + "=" + value);
            }
        }
    }
}