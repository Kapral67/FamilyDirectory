package org.familydirectory.assets.lambda.function.api.carddav.resource;

public sealed
interface IMemberResource extends IResource permits MemberResource, DeletedMemberResource {
}
