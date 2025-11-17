package org.familydirectory.assets.lambda.function.api.carddav.resource;

import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.ADDRESS_BOOK_PATH;

public sealed
interface IMemberResource extends IResource permits PresentMemberResource, DeletedMemberResource {
    default
    String getHref() {
        return ADDRESS_BOOK_PATH + this.getName();
    }
}
