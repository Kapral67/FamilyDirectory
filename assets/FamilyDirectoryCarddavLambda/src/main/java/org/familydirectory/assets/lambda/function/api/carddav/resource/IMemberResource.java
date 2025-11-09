package org.familydirectory.assets.lambda.function.api.carddav.resource;

import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.CONTACTS_COLLECTION_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.URL;

public sealed
interface IMemberResource extends IResource permits PresentMemberResource, DeletedMemberResource {
    default
    String getHref() {
        return URL + CONTACTS_COLLECTION_PATH + this.getName();
    }
}
