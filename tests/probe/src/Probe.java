public class Probe
{
    public static void main(String[] args)
    {
        System.out.println("hello");

        System.out.println("vm.name    = " + System.getProperty("java.vm.name"));
        System.out.println("vm.version = " + System.getProperty("java.vm.version"));
        System.out.println("java.version = " + System.getProperty("java.version"));
    }
}
