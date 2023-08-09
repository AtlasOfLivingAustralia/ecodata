let mapping = {
    'vegetation mapping':[15],
    'floristics':[15],
    'plant tissue vouchering':[15],
    'cover':[15],
    'basal area':[15],
    'coarse woody debris':[15],
    'recruitment':[15],
    'soils':[30],
    'vertebrate fauna':[13, 23],
    'invertebrate fauna':[13],
    'condition':[15],
    40: [13],
    41: [15],
    42: [15],
    3: [13,15,23, 30],
    13: [13,15,23, 30],
    36: [13,15,23, 30],
    37: [13,15,23, 30]
};

let value = JSON.stringify(mapping);
let key = 'paratoo.service_protocol_mapping'
let setting = db.setting.findOne({key:key});
if (setting) {
    setting.value = value;
    db.setting.replaceOne({key:key}, setting);
}
else {
    db.setting.insertOne({key:key, value:value});
}