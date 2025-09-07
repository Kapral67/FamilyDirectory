package org.familydirectory.assets.lambda.function.api.carddav.resource;

public abstract non-sealed
class AbstractResourceObject implements IResource {
    @Override
    public final
    boolean equals (final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (this.getClass() != o.getClass()) {
            if (!(this instanceof IMemberResource && o instanceof IMemberResource)) {
                return false;
            }
        }

        return this.getName().equals(((AbstractResourceObject) o).getName());
    }
}
