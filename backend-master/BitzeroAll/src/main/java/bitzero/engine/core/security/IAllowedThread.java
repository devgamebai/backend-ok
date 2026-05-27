package bitzero.engine.core.security;

public interface IAllowedThread {
     String getName();

     ThreadComparisonType getComparisonType();
}
