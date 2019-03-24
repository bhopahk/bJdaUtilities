import me.bhop.bjdautilities.command.result.CommandResult;

public interface CustomResults {

    static CoolResult coolResult(String value) {
        return new CoolResult(value);
    }

    class CoolResult implements CommandResult {
        public String value;

        public CoolResult(String value) {
            this.value = value;
        }
    }
}
