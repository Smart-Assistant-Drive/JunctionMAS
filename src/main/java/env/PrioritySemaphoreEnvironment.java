package env;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;

import java.util.Collection;

public class PrioritySemaphoreEnvironment extends Environment {

    // we have a primary road (strada primaria) and a secondary road (strada secondaria) that need to be regulated with a priority for primary road

    @Override
    public Collection<Literal> getPercepts(String agName) {
        return super.getPercepts(agName);
    }

    @Override
    public boolean executeAction(String agName, Structure act) {
        return super.executeAction(agName, act);
    }
}
